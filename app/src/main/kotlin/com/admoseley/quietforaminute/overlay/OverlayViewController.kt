package com.admoseley.quietforaminute.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import com.admoseley.quietforaminute.service.DndController
import com.admoseley.quietforaminute.ui.overlay.MuteDurationDialog
import com.admoseley.quietforaminute.ui.theme.QuietTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Owns the single system-overlay window that hosts [MuteDurationDialog].
 *
 * All methods must be called on the main thread (they are, from [com.admoseley.quietforaminute.service.OverlayService]).
 *
 * Re-entrancy: [show] flips [isShowing] *synchronously* before it suspends to read preferences.
 * The previous version only set the flag after the async work, so two broadcasts arriving close
 * together could both pass the guard and add two windows — the second overwrote the reference to
 * the first, which then could never be removed.
 */
class OverlayViewController(
    private val context: Context,
    private val prefsRepository: PreferencesRepository,
    private val dndController: DndController,
    private val onDurationSelected: (
        hours: Int, minutes: Int, restoreVolume: Int, streamType: Int, dndEnabled: Boolean
    ) -> Unit
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var showing = false
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null

    fun isShowing(): Boolean = showing

    /**
     * Shows the popup for [streamType]. The restore slider is sized to that stream's own maximum
     * (ring max is usually lower than music max), and the default from Settings — stored in music
     * units — is scaled to match.
     */
    fun show(streamType: Int) {
        if (showing) return
        showing = true

        val owner = ServiceLifecycleOwner().also { lifecycleOwner = it }
        owner.onCreate()

        scope.launch {
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val defaultMusicUnits = prefsRepository.defaultVolume.first()
            val initialRestoreVolume = if (streamType == AudioManager.STREAM_MUSIC) {
                defaultMusicUnits
            } else {
                (defaultMusicUnits.toFloat() / musicMax * maxVolume).roundToInt()
            }.coerceIn(0, maxVolume)

            // Read once here rather than inside the composable: the popup is short-lived and the
            // grant cannot change while it is on screen.
            val dndAvailable = dndController.canControlDnd()
            val initialDndEnabled = prefsRepository.dndWithMute.first()

            // dismiss() may have run while we were suspended reading DataStore.
            if (!showing) return@launch

            val view = ComposeView(context).apply {
                // ComposeView needs all three owners on the view tree before setContent().
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    QuietTheme {
                        MuteDurationDialog(
                            initialRestoreVolume = initialRestoreVolume,
                            maxVolume = maxVolume,
                            initialDndEnabled = initialDndEnabled,
                            dndAvailable = dndAvailable,
                            onConfirm = { h, m, v, dnd ->
                                onDurationSelected(h, m, v, streamType, dnd)
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
                // The only overlay type available to third-party apps since API 26. It is drawn
                // above other apps but NOT above the lock screen.
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                windowManager.addView(view, params)
            } catch (e: Exception) {
                // Typically BadTokenException when the overlay permission was revoked mid-flight.
                Log.w(TAG, "Unable to add overlay window", e)
                owner.destroy()
                lifecycleOwner = null
                showing = false
                return@launch
            }
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
        }
        overlayView = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        showing = false
    }

    /** Call from the owning service's onDestroy. */
    fun release() {
        dismiss()
        scope.cancel()
    }

    private companion object {
        const val TAG = "OverlayViewController"
    }
}
