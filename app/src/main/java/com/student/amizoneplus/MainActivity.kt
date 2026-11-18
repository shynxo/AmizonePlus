package com.student.amizoneplus

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.student.amizoneplus.ui.AppRoot
import com.student.amizoneplus.ui.AppViewModel

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        preferHighestRefreshRate()
        setContent { AppRoot(vm) }
        com.student.amizoneplus.work.KeepAlive.schedule(this, 60_000L) // (re)start the session keep-alive
    }

    /**
     * Some phones (OnePlus, Xiaomi…) run apps at 60 Hz unless they ask for more.
     * Ask for the display's fastest mode at the current resolution (e.g. 120 Hz).
     */
    @Suppress("DEPRECATION")
    private fun preferHighestRefreshRate() {
        val display = (if (Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay) ?: return
        val cur = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when coming back to the app if the data is a few minutes old.
        val d = vm.state.value.data
        if (d != null && System.currentTimeMillis() - d.fetchedAt > 5 * 60_000) vm.refresh()
    }
}
