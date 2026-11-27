package com.student.amizoneplus.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.MenuGlyph
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.launch
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlin.math.roundToInt

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

/** Kept for callers that time things against the menu. */
const val MENU_OPEN_MS = 420
const val MENU_CLOSE_MS = 340

/**
 * The quick menu.
 *
 * The live app blurs behind it (full strength down to just past the last option, then fading
 * out), the options drop in one after another on a bouncy spring, and the button stays exactly
 * where it was, its grid of dots folding into a cross. Tap outside the options to close.
 *
 * The options are one connected list. Opening a section splits the list around it: gaps open
 * above and below, the section becomes its own rounded card, its pages un-blur into view and
 * its arrow turns. Only one section is open at a time.
 */
@Composable
fun QuickMenuOverlay(
    open: Boolean,
    progress: () -> Float,
    anchor: Rect?,
    hazeState: dev.chrisbanes.haze.HazeState,
    onClose: () -> Unit,
    openPage: (String, String) -> Unit,
) {
    val haptics = LocalHaptics.current
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var screen by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    // Where the button sits; fall back to a typical top-right spot.
    val btn = anchor ?: with(density) {
        val sz = 52.dp.toPx(); val cx = screen.width - L.page.toPx() - sz / 2; val cy = (statusTop + 10.dp + 30.dp).toPx()
        Rect(cx - sz / 2, cy - sz / 2, cx + sz / 2, cy + sz / 2)
    }
    var listBottom by remember { mutableFloatStateOf(0f) }

    var openGroup by remember { mutableStateOf<Int?>(null) }
    val sel = remember { List(MENU_GROUPS.size) { Animatable(0f) } }
    LaunchedEffect(openGroup) {
        sel.forEachIndexed { i, a ->
            launch { a.animateTo(if (i == openGroup) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 380f)) }
        }
    }
    val scroll = rememberScrollState()

    // Rows drop in one after another on a bouncy spring (skiper-style) and fold away quickly.
    val rowIn = remember { List(MENU_GROUPS.size) { Animatable(0f) } }
    LaunchedEffect(open) {
        rowIn.forEachIndexed { i, a ->
            launch {
                if (open) {
                    kotlinx.coroutines.delay(40L + i * 35L)
                    a.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
                } else {
                    kotlinx.coroutines.delay((rowIn.size - 1 - i) * 18L)
                    a.animateTo(0f, spring(dampingRatio = 1f, stiffness = 900f))
                }
            }
        }
    }
    val fadeLen = with(density) { 150.dp.toPx() }

    Box(
        Modifier.fillMaxSize()
            .onSizeChanged { screen = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat()) }
            .clickable(remember { MutableInteractionSource() }, indication = null) { onClose() },
    ) {
        // ── The live app behind, blurred down to just past the last option, then fading out ──
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
                .hazeBlur(
                    dev.chrisbanes.haze.HazeInput.Backdrop(hazeState),
                    HazeBlurStyle {
                        blurRadius(24.dp)
                        // A black tint (not the page colour) so the blur reads deep, not milky.
                        colorEffects(listOf(HazeColorEffect.tint(Color.Black.copy(alpha = if (P.cur.dark) 0.32f else 0.14f))))
                        val edge = blurEdge(btn, listBottom, progress())
                        progressive(dev.chrisbanes.haze.HazeProgressive.verticalGradient(
                            startY = edge, startIntensity = 1f, endY = edge + fadeLen, endIntensity = 0f,
                        ))
                    },
                )
                .drawBehind {
                    // A black gradient that follows the blur: darkest at the top, easing off to its edge.
                    val edge = blurEdge(btn, listBottom, progress())
                    val k = if (P.cur.dark) 1f else 0.45f
                    drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.62f * k),
                        (edge / size.height).coerceIn(0f, 1f) to Color.Black.copy(alpha = 0.38f * k),
                        ((edge + fadeLen) / size.height).coerceIn(0f, 1f) to Color.Transparent,
                    ))
                },
        )

        Box(
            Modifier.fillMaxSize()
                // Swipe up anywhere on the menu to close.
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total < -48.dp.toPx()) onClose() }) { _, dy -> total += dy }
                },
        ) {
            // Title, level with the button.
            val titleTop = with(density) { btn.center.y.toDp() } - 16.dp
            T("AMIZONE+", Type.headline, modifier = Modifier
                .padding(start = L.page, top = titleTop.coerceAtLeast(0.dp))
                .graphicsLayer {
                    val a = progress().coerceIn(0f, 1f)
                    alpha = ((a - 0.2f) / 0.4f).coerceIn(0f, 1f)
                    translationX = -12.dp.toPx() * (1f - a)
                })

            val listTop = with(density) { btn.bottom.toDp() } + 18.dp
            Column(
                Modifier.fillMaxWidth()
                    .padding(top = listTop)
                    .heightIn(max = with(density) { screen.height.toDp() } - listTop)
                    .verticalScroll(scroll)
                    .padding(start = L.page, end = L.page, bottom = navBottom + 24.dp),
            ) {
                Column(
                    Modifier
                        .clickable(remember { MutableInteractionSource() }, indication = null) {}
                        .onGloballyPositioned { listBottom = it.boundsInWindow().bottom },
                ) {
                    MENU_GROUPS.forEachIndexed { i, g ->
                        val prev = if (i > 0) sel[i - 1] else null
                        val next = if (i < MENU_GROUPS.lastIndex) sel[i + 1] else null
                        MenuRow(
                            group = g,
                            index = i,
                            sel = { sel[i].value },
                            splitAbove = { if (prev == null) 1f else maxOf(sel[i].value, prev.value).coerceAtLeast(0f) },
                            splitBelow = { if (next == null) 1f else maxOf(sel[i].value, next.value).coerceAtLeast(0f) },
                            appear = { rowIn[i].value },
                            onToggle = {
                                if (openGroup == i) { haptics.collapse(); openGroup = null } else { haptics.expand(); openGroup = i }
                            },
                            openPage = openPage,
                        )
                    }
                }
            }
        }

        // ── The button, exactly where it was, dots folding into a cross ──
        with(density) {
            Box(
                Modifier
                    .offset { IntOffset(btn.left.roundToInt(), btn.top.roundToInt()) }
                    .size(btn.width.toDp(), btn.height.toDp())
                    .press(onClose, Feel.Select, 0.9f)
                    .background(P.cream, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                MenuGlyph({ progress() }, P.onCream, Modifier.size(btn.width.toDp() * 0.44f))
            }
        }
    }
}

/** Where the blur's full-strength part ends: grows from the button down to just past the last option. */
private fun blurEdge(btn: Rect, listBottom: Float, p: Float): Float {
    val rest = if (listBottom > 0f) listBottom + 12f else btn.bottom
    return btn.bottom + (rest - btn.bottom) * p.coerceIn(0f, 1f)
}

/** One section of the list, plus its pages when open. */
@Composable
private fun MenuRow(
    group: MenuGroup,
    index: Int,
    sel: () -> Float,
    splitAbove: () -> Float,
    splitBelow: () -> Float,
    appear: () -> Float,
    onToggle: () -> Unit,
    openPage: (String, String) -> Unit,
) {
    val gap = 10.dp
    val r = 22.dp
    Box(
        Modifier
            .layout { m, c ->
                // Gaps open above / below as the list splits around the open section.
                val top = if (index == 0) 0 else (gap.toPx() * splitAbove() * 0.5f).roundToInt()
                val bottom = if (index == MENU_GROUPS.lastIndex) 0 else (gap.toPx() * splitBelow() * 0.5f).roundToInt()
                val p = m.measure(c)
                layout(p.width, p.height + top + bottom) { p.place(0, top) }
            }
            .graphicsLayer {
                val a = appear()
                alpha = a.coerceIn(0f, 1f)
                translationY = -18.dp.toPx() * (1f - a)   // drops in from above, overshoots, settles
                val sc = 0.94f + 0.06f * a
                scaleX = sc; scaleY = sc
            }
            .drawBehind {
                val k = sel()
                val tr = r.toPx() * (if (index == 0) 1f else splitAbove())
                val br = r.toPx() * (if (index == MENU_GROUPS.lastIndex) 1f else splitBelow())
                drawPath(com.student.amizoneplus.ui.kit.squirclePath(Rect(0f, 0f, size.width, size.height), tr, tr, br, br), lerp(P.card, P.cardRaised, k.coerceIn(0f, 1f)))
            },
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
                    .padding(horizontal = L.pad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(group.icon, null, tint = P.lav, modifier = Modifier.size(20.dp))
                T(group.title, Type.label, modifier = Modifier.weight(1f))
                T("${group.items.size}", Type.caption, sub = true, modifier = Modifier.graphicsLayer { alpha = 1f - sel() })
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = P.sub,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f * sel() })
            }
            // Pages: height grows with the split; they un-blur and rise into place.
            Box(
                Modifier
                    .layout { m, c ->
                        val k = sel()
                        if (k <= 0.001f) return@layout layout(c.minWidth, 0) {}
                        val p = m.measure(c)
                        layout(p.width, (p.height * k).roundToInt().coerceAtLeast(0)) { p.place(0, 0) }
                    }
                    .clipToBounds()
                    .graphicsLayer {
                        val k = sel().coerceIn(0f, 1f)
                        alpha = ((k - 0.25f) / 0.6f).coerceIn(0f, 1f)
                        translationY = -8.dp.toPx() * (1f - k)
                        val b = 14.dp.toPx() * (1f - k)
                        renderEffect = if (android.os.Build.VERSION.SDK_INT >= 31 && b > 0.5f)
                            android.graphics.RenderEffect.createBlurEffect(b, b, android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
                        else null
                    },
            ) {
                Column(Modifier.padding(start = L.pad, end = L.pad, bottom = 8.dp)) {
                    group.items.forEachIndexed { j, it ->
                        if (j > 0) HorizontalDivider(color = P.line, thickness = 0.5.dp)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 54.dp).press({ openPage(it.path, it.label) }, Feel.Tap),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(it.icon, null, tint = P.sub, modifier = Modifier.size(19.dp))
                            T(it.label, Type.body, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = P.faint, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
