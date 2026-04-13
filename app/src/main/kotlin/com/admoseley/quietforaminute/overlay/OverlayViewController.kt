package com.admoseley.quietforaminute.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.admoseley.quietforaminute.ui.overlay.MuteDurationDialog
import com.admoseley.quietforaminute.ui.theme.QuietTheme

class OverlayViewController(
    private val context: Context,
    private val onDurationSelected: (hours: Int, minutes: Int) -> Unit
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null

    fun show() {
        if (overlayView != null) return

        val owner = ServiceLifecycleOwner()
        lifecycleOwner = owner

        // Proper lifecycle progression: CREATED → STARTED → RESUMED
        owner.onCreate()

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                QuietTheme {
                    MuteDurationDialog(
                        onConfirm = { h, m ->
                            onDurationSelected(h, m)
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

        // Move to STARTED then RESUMED after the view is attached
        owner.onStart()
        owner.onResume()
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
