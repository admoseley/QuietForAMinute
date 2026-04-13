package com.admoseley.quietforaminute.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.media.AudioManager
import androidx.compose.ui.platform.ComposeView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import com.admoseley.quietforaminute.ui.overlay.MuteDurationDialog
import com.admoseley.quietforaminute.ui.theme.QuietTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayViewController(
    private val context: Context,
    private val prefsRepository: PreferencesRepository,
    private val onDurationSelected: (hours: Int, minutes: Int, restoreVolume: Int) -> Unit
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null

    fun show() {
        if (overlayView != null) return

        val owner = ServiceLifecycleOwner()
        lifecycleOwner = owner

        owner.onCreate()

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            val initialRestoreVolume = prefsRepository.defaultVolume.first()
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    QuietTheme {
                        MuteDurationDialog(
                            initialRestoreVolume = initialRestoreVolume,
                            maxVolume = maxVolume,
                            onConfirm = { h, m, v ->
                                onDurationSelected(h, m, v)
                                dismiss()
                            },
                            onDismiss = { dismiss() }
                        )
                    }
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(view, params)
            overlayView = view

            owner.onStart()
            owner.onResume()
        }
    }

    fun dismiss() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) { /* view already removed */ }
            overlayView = null
        }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }

    fun isShowing(): Boolean = overlayView != null
}
