package com.student.amizoneplus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.student.amizoneplus.data.AmizoneClient
import com.student.amizoneplus.logic.earnedBadges
import com.student.amizoneplus.ui.screens.AuthedWebPage
import com.student.amizoneplus.ui.screens.BadgeGallery
import com.student.amizoneplus.ui.screens.Celebration
import com.student.amizoneplus.ui.screens.LmsScreen
import com.student.amizoneplus.ui.screens.LoginScreen
import com.student.amizoneplus.ui.screens.MENU_CLOSE_MS
import com.student.amizoneplus.ui.screens.MENU_OPEN_MS
import com.student.amizoneplus.ui.screens.MoreScreen
import com.student.amizoneplus.ui.screens.QuickMenuOverlay
import com.student.amizoneplus.ui.screens.ScheduleScreen
import com.student.amizoneplus.ui.screens.SubjectsScreen
import com.student.amizoneplus.ui.screens.TodayScreen
import com.student.amizoneplus.ui.screens.WebLoginSheet
import com.student.amizoneplus.ui.screens.screenExit
import com.student.amizoneplus.ui.shell.ErrorBanner
import com.student.amizoneplus.ui.shell.LocalMenu
import com.student.amizoneplus.ui.shell.MenuController
import com.student.amizoneplus.ui.shell.BottomNav
import com.student.amizoneplus.ui.shell.Route
import com.student.amizoneplus.ui.theme.Haptics
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

@Composable
fun AppRoot(vm: AppViewModel) {
    // Light/dark follows the phone; a change recreates the activity, so this runs first.
    com.student.amizoneplus.ui.theme.P.applyTheme(androidx.compose.foundation.isSystemInDarkTheme())
    val s by vm.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val haptics = remember { Haptics(view) { vm.state.value.settings.haptics } }

    CompositionLocalProvider(LocalHaptics provides haptics) {
        LaunchedEffect(Unit) {
            vm.feedback.collect {
                when (it) {
                    Feedback.Success -> haptics.success()
                    Feedback.Warning -> haptics.warning()
                    Feedback.Error -> haptics.error()
                }
            }
        }

        Box(Modifier.fillMaxSize().background(P.canvas)) {
            when (s.status) {
                Status.Booting -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P.ink) }
                Status.SignedOut -> LoginScreen(vm, s.error)
                Status.SignedIn -> Main(vm, s)
            }
            if (s.webLoginOpen) {
                val c = vm.pendingCredentials()
                WebLoginSheet(c?.username, c?.password, onDone = vm::webLoginFinished, onClose = vm::closeWebLogin)
            }
        }
    }
}

@Composable
private fun Main(vm: AppViewModel, s: UiState) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHaptics.current
    var route by remember { mutableStateOf(Route.Today) }
    var lmsFocus by remember { mutableStateOf<String?>(null) }
    var web by remember { mutableStateOf<Pair<String, String>?>(null) }
    var gallery by remember { mutableStateOf(false) }
    var scheduleFocus by remember { mutableStateOf<String?>(null) }
    var resultsFocus by remember { mutableStateOf(false) }
    var lastWeb by remember { mutableStateOf<Pair<String, String>?>(null) }
    val hazeState = rememberHazeState()
    // The pets live across the whole app; this is their world.
    val pets = remember { com.student.amizoneplus.ui.pets.PetWorld() }
    pets.route = route

    // Quick menu: one progress value (0 = closed, 1 = open) drives the whole choreography.
    var menuMounted by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val m = remember { Animatable(0f) }
    // The screen's ease-back and the blur run on their own smooth curve (the rows' timer is linear).
    val z = remember { Animatable(0f) }
    // The menu blurs everything below it: screens, status bar and nav (with their own blurs).
    val menuHaze = rememberHazeState()
    fun openMenu() {
        if (menuOpen) return
        haptics.expand(); menuMounted = true; menuOpen = true
        scope.launch { m.animateTo(1f, tween(MENU_OPEN_MS, easing = LinearEasing)) }
        scope.launch { z.animateTo(1f, tween(620, easing = Motion.emphasized)) }
    }
    fun closeMenu(after: (() -> Unit)? = null) {
        if (!menuOpen) { after?.invoke(); return }
        haptics.collapse(); menuOpen = false
        // Blur holds while the rows fold away, then falls off at the end.
        scope.launch {
            // Rows fold quickly; the background eases back over its usual 0.5 s. Unmount after both.
            val bg = launch { z.animateTo(0f, tween(500, easing = Motion.accelerate)) }
            m.animateTo(0f, tween(MENU_CLOSE_MS, easing = LinearEasing))
            bg.join()
            if (!menuOpen) menuMounted = false
            after?.invoke()
        }
    }
    BackHandler(enabled = menuOpen) { closeMenu() }

    fun go(r: Route) { if (menuOpen) closeMenu(); route = r }
    val openWeb: (String, String) -> Unit = { url, title -> web = url to title }

    val banner: (@Composable () -> Unit)? = s.error?.let { err ->
        { ErrorBanner(err, if (s.needsWebLogin) "Sign in →" else "Retry") { if (s.needsWebLogin) vm.openWebLogin() else vm.refresh(true) } }
    }

    pets.switchTab = { r -> go(r) }

    // Updates from GitHub Releases: checked quietly on launch (every few hours at most),
    // or on demand from More → About.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var update by remember { mutableStateOf<com.student.amizoneplus.data.Updater.Release?>(null) }
    LaunchedEffect(Unit) {
        val u = com.student.amizoneplus.data.Updater
        if (u.enabled && u.shouldAutoCheck(ctx)) {
            kotlinx.coroutines.delay(2500) // let the first refresh go first
            runCatching { u.check() }.onSuccess { r ->
                u.markChecked(ctx)
                if (r != null && !u.snoozed(ctx, r.version)) update = r
            }
        }
    }
    val checkUpdates: () -> Unit = {
        val u = com.student.amizoneplus.data.Updater
        if (!u.enabled) android.widget.Toast.makeText(ctx, "Updates aren't set up in this build.", android.widget.Toast.LENGTH_SHORT).show()
        else scope.launch {
            runCatching { u.check() }
                .onSuccess { r ->
                    u.markChecked(ctx)
                    if (r != null) update = r
                    else android.widget.Toast.makeText(ctx, "You're on the latest version (v${u.current}).", android.widget.Toast.LENGTH_SHORT).show()
                }
                .onFailure { android.widget.Toast.makeText(ctx, it.message ?: "Couldn't check for updates.", android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
    CompositionLocalProvider(
        LocalMenu provides MenuController(menuOpen, ::openMenu) { closeMenu() },
        com.student.amizoneplus.ui.pets.LocalPetWorld provides pets,
    ) {
        Box(Modifier.fillMaxSize()) {
          // Everything here (screens + status bar + nav) is what the quick menu blurs.
          Box(Modifier.fillMaxSize().hazeSource(menuHaze)) {
            // Everything under the nav is a blur source for the nav's progressive blur.
            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    val sc = 1f - 0.04f * z.value
                    scaleX = sc; scaleY = sc
                }) {
                    AnimatedContent(
                        route,
                        transitionSpec = {
                            (fadeIn(tween(260, easing = Motion.emphasized)) + slideInVertically(tween(380, easing = Motion.emphasized)) { it / 80 }) togetherWith
                                fadeOut(tween(140))
                        },
                        label = "route",
                    ) { r ->
                        CompositionLocalProvider(com.student.amizoneplus.ui.pets.LocalPetRoute provides r) {
                        when (r) {
                            Route.Today -> TodayScreen(s, ::go, { vm.refresh(true) }, { gallery = true }, banner)
                            Route.Subjects -> SubjectsScreen(s, { vm.refresh(true) }, { code -> lmsFocus = code; go(Route.Lms) }, vm::updateSettings, banner)
                            Route.Lms -> LmsScreen(vm, s, lmsFocus, { lmsFocus = null }, openWeb)
                            Route.Schedule -> ScheduleScreen(s, { vm.refresh(true) }, banner, scheduleFocus) { scheduleFocus = null }
                            Route.More -> MoreScreen(s, { vm.refresh(true) }, vm::updateSettings, { gallery = true }, openWeb, vm::signOut, banner, resultsFocus, { resultsFocus = false }, checkUpdates)
                        }
                        }
                    }
                }
            }
            com.student.amizoneplus.ui.shell.StatusBlur(hazeState)
            com.student.amizoneplus.ui.shell.BottomNav(route, ::go, hazeState)
          }
            // Pets roam above the screens and the nav (so they can be carried onto a tab).
            com.student.amizoneplus.ui.pets.PetLayer(pets, s.settings.pets, active = !menuMounted && web == null && !gallery && s.celebrate == null)

            if (menuMounted) QuickMenuOverlay({ m.value }, { z.value }, menuHaze, { closeMenu() }) { path, title ->
                // Sections the app already shows natively open in the app's own UI.
                when (path.lowercase()) {
                    "/examination/examschedule" -> { scheduleFocus = "exams"; go(Route.Schedule) }
                    "/timetable/home" -> { scheduleFocus = "classes"; go(Route.Schedule) }
                    "/examination/examination" -> { resultsFocus = true; go(Route.More) }
                    else -> web = (AmizoneClient.BASE + path) to title
                }
            }

            // Status-bar icons: light over the dark home, dark over bright hero colours.
            val window = (androidx.compose.ui.platform.LocalContext.current as? android.app.Activity)?.window
            val view = androidx.compose.ui.platform.LocalView.current
            val lightBars = if (route == Route.Today) !P.cur.dark else true
            androidx.compose.runtime.SideEffect {
                window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = lightBars }
            }

            AnimatedVisibility(web != null, enter = slideInVertically(tween(380, easing = Motion.emphasized)) { it } + fadeIn(), exit = slideOutVertically(tween(260)) { it } + fadeOut()) {
                // Keep the last page drawn while it slides away.
                web?.let { lastWeb = it }
                lastWeb?.let { (url, title) -> androidx.compose.runtime.key(url) { AuthedWebPage(url, title, vm::relogin) { web = null } } }
            }
            if (gallery) BadgeGallery(s.data?.let { earnedBadges(it, s.settings.threshold) }.orEmpty()) { gallery = false }
            s.celebrate?.let { Celebration(it, vm::dismissCelebration) }
            update?.let { r ->
                com.student.amizoneplus.ui.screens.UpdateSheet(r) {
                    com.student.amizoneplus.data.Updater.snooze(ctx, r.version); update = null
                }
            }
        }
    }
}
