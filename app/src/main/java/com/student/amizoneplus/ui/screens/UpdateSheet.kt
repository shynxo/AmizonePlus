package com.student.amizoneplus.ui.screens

import com.student.amizoneplus.ui.kit.SquircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.student.amizoneplus.data.Updater
import com.student.amizoneplus.ui.kit.Btn
import com.student.amizoneplus.ui.kit.BtnKind
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Update available" sheet: what's new, then Update → download (with progress) →
 * Android's installer. The first time, Android asks to allow installs from this app;
 * the sheet waits and carries on by itself once you come back.
 */
@Composable
fun UpdateSheet(rel: Updater.Release, onLater: () -> Unit) {
    val ctx = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(-1f) } // -1 idle, 0..1 downloading
    var file by remember { mutableStateOf<File?>(null) }
    var waitingPermission by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val enter = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(520, easing = Motion.emphasized)) }

    fun installNow(f: File) {
        if (!Updater.canInstall(ctx)) { waitingPermission = true; Updater.openInstallPermission(ctx); return }
        waitingPermission = false
        Updater.install(ctx, f)
    }

    fun start() {
        error = null
        file?.let { installNow(it); return }
        haptics.press()
        progress = 0f
        scope.launch {
            runCatching { Updater.download(ctx, rel) { progress = it } }
                .onSuccess { f -> file = f; progress = 1f; haptics.success(); installNow(f) }
                .onFailure { progress = -1f; error = it.message ?: "Download failed."; haptics.error() }
        }
    }

    // Back from the "allow installs" screen: carry on if it was granted.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(waitingPermission) {
        if (!waitingPermission) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (Updater.canInstall(ctx)) file?.let { installNow(it) }
        }
    }

    val busy = progress in 0f..0.999f
    BackHandler(enabled = !busy, onBack = onLater)
    val shown by animateFloatAsState(if (busy) 1f else 0f, tween(260), label = "bar")

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = enter.value }
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { if (!busy) onLater() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .graphicsLayer { translationY = (1f - enter.value) * 220.dp.toPx() }
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .background(P.card, SquircleShape(topStart = L.sheetRadius, topEnd = L.sheetRadius))
                .navigationBarsPadding()
                .padding(horizontal = L.pad, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Caps("UPDATE AVAILABLE", P.coral)
            T("V${rel.version}", Type.display)
            T("You're on v${Updater.current}" + (if (rel.sizeBytes > 0) " · %.1f MB".format(rel.sizeBytes / 1_048_576f) else ""), Type.caption, sub = true)
            if (rel.notes.isNotBlank()) Column(
                Modifier.fillMaxWidth().heightIn(max = 220.dp).padding(top = 6.dp)
                    .background(P.cardRaised, SquircleShape(20.dp)).verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                Caps("WHAT'S NEW", P.faint, Modifier.padding(bottom = 8.dp))
                T(rel.notes.replace("\r", ""), Type.body)
            }
            // download progress
            Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).graphicsLayer { alpha = shown }
                .background(P.cardRaised, SquircleShape(50))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).background(P.coral, SquircleShape(50)))
            }
            val status = when {
                error != null -> error!!
                waitingPermission -> "Allow \"Install unknown apps\" for Amizone+, then come back — it continues by itself."
                busy -> "Downloading… ${(progress * 100).toInt()}%"
                file != null -> "Downloaded. Tap Install and confirm."
                else -> "Your login and settings stay as they are."
            }
            T(status, Type.caption, color = if (error != null) P.bad else P.sub)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Later", BtnKind.Ghost, null, if (busy) null else onLater)
                Btn(
                    when { file != null -> "Install"; busy -> "Downloading"; else -> "Update" },
                    BtnKind.Lav, if (file != null) Icons.Outlined.InstallMobile else Icons.Outlined.Download,
                    if (busy) null else ({ start() }),
                )
            }
        }
    }
}
