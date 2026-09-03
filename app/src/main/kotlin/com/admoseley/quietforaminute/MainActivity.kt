package com.admoseley.quietforaminute

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.admoseley.quietforaminute.service.OverlayService
import com.admoseley.quietforaminute.ui.navigation.AppNavigation
import com.admoseley.quietforaminute.ui.settings.SettingsViewModel
import com.admoseley.quietforaminute.ui.theme.QuietTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Activity-scoped instance used only for the theme. SettingsScreen gets its own
    // nav-entry-scoped SettingsViewModel via hiltViewModel(); both read the same DataStore so
    // they stay consistent, but a dedicated ThemeViewModel would make the intent clearer.
    private val viewModel: SettingsViewModel by viewModels()

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently; permissions card in Settings screen shows status */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()
        startOverlayService()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            QuietTheme(darkTheme = isDarkTheme) {
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
        // Starting from a visible Activity is always permitted; OverlayService is START_STICKY and
        // is also (re)started by BootReceiver, so this call mostly matters on first launch.
        startForegroundService(Intent(this, OverlayService::class.java))
    }
}
