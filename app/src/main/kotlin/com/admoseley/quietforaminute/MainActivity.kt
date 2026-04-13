package com.admoseley.quietforaminute

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.admoseley.quietforaminute.service.OverlayService
import com.admoseley.quietforaminute.ui.navigation.AppNavigation
import com.admoseley.quietforaminute.ui.theme.QuietTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently; permissions card in Settings screen shows status */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()
        startOverlayService()

        setContent {
            QuietTheme {
                AppNavigation()
            }
        }
    }

    private fun requestRequiredPermissions() {
        // POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Exact alarm permission: user grants from Settings when needed (see Permissions section).
    }

    private fun startOverlayService() {
        startForegroundService(Intent(this, OverlayService::class.java))
    }
}
