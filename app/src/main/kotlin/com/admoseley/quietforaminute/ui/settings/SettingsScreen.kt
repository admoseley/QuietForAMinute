package com.admoseley.quietforaminute.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.admoseley.quietforaminute.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val defaultVolume by viewModel.defaultVolume.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val chimeOnMute by viewModel.chimeOnMute.collectAsStateWithLifecycle()
    val chimeOnRestore by viewModel.chimeOnRestore.collectAsStateWithLifecycle()
    val muteChimeUri by viewModel.muteChimeUri.collectAsStateWithLifecycle()
    val restoreChimeUri by viewModel.restoreChimeUri.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    // The ringtone picker returns a null URI when the user picks "Silent". A null URI is also how
    // we represent "use the bundled chime", so Silent used to be indistinguishable from Default.
    // Map Silent to "chime off" instead, which is what the user meant.
    val muteChimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.let { intent ->
                IntentCompat.getParcelableExtra(intent, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            viewModel.setMuteChimeUri(uri?.toString())
            if (uri == null) viewModel.setChimeOnMute(false)
        }
    }

    val restoreChimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.let { intent ->
                IntentCompat.getParcelableExtra(intent, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            viewModel.setRestoreChimeUri(uri?.toString())
            if (uri == null) viewModel.setChimeOnRestore(false)
        }
    }

    fun pickRingtone(isMute: Boolean) {
        val currentUri = if (isMute) muteChimeUri else restoreChimeUri
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, if (isMute) "Mute Chime" else "Restore Chime")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri?.toUri())
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        }
        if (isMute) muteChimeLauncher.launch(intent) else restoreChimeLauncher.launch(intent)
    }

    val hasOverlayPermission = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    val canScheduleExactAlarms = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    // Query the device's actual max volume (varies by manufacturer)
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // Refresh permission state on every ON_RESUME (e.g. returning from system settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission.value = Settings.canDrawOverlays(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    canScheduleExactAlarms.value = alarmManager.canScheduleExactAlarms()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Hero card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Mute when you need it.\nRestore when you forget.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = MaterialTheme.typography.titleLarge.lineHeight
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Start
                        )
                    }
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp)
                    )
                }
            }

            // Default volume
            SectionLabel("Restore Volume")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (defaultVolume == 0) Icons.AutoMirrored.Filled.VolumeOff
                            else if (defaultVolume < maxVolume / 2) Icons.AutoMirrored.Filled.VolumeDown
                            else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Default Volume",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${(defaultVolume.toFloat() / maxVolume * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = defaultVolume.toFloat(),
                        onValueChange = { viewModel.setDefaultVolume(it.roundToInt()) },
                        valueRange = 0f..maxVolume.toFloat(),
                        steps = maxVolume - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Volume restored to this level when a mute timer expires",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Behavior settings
            SectionLabel("App Settings")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("App Theme") },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                ThemeOption("Light", "LIGHT", themeMode) { viewModel.setThemeMode(it) }
                                ThemeOption("Dark", "DARK", themeMode) { viewModel.setThemeMode(it) }
                                ThemeOption("System", "SYSTEM", themeMode) { viewModel.setThemeMode(it) }
                            }
                        },
                        leadingContent = {
                            Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Show timer popup on mute") },
                        supportingContent = { Text("Opens a dialog when volume is set to 0") },
                        leadingContent = {
                            Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Switch(
                                checked = overlayEnabled,
                                onCheckedChange = { viewModel.setOverlayEnabled(it) }
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Chime on mute") },
                        supportingContent = {
                            // Note: getRingtone()/getTitle() query a content provider on the main
                            // thread. It is a one-off per URI change so the jank is negligible,
                            // but it would belong in the ViewModel if this list grows.
                            val name = remember(muteChimeUri) {
                                muteChimeUri?.let { RingtoneManager.getRingtone(context, it.toUri())?.getTitle(context) } ?: "Default chime"
                            }
                            Text("Sound: $name")
                        },
                        leadingContent = {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Switch(
                                checked = chimeOnMute,
                                onCheckedChange = { viewModel.setChimeOnMute(it) }
                            )
                        },
                        modifier = Modifier.clickable { pickRingtone(true) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Chime on restore") },
                        supportingContent = {
                            val name = remember(restoreChimeUri) {
                                restoreChimeUri?.let { RingtoneManager.getRingtone(context, it.toUri())?.getTitle(context) } ?: "Default chime"
                            }
                            Text("Sound: $name")
                        },
                        leadingContent = {
                            Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Switch(
                                checked = chimeOnRestore,
                                onCheckedChange = { viewModel.setChimeOnRestore(it) }
                            )
                        },
                        modifier = Modifier.clickable { pickRingtone(false) }
                    )
                }
            }

            // Permissions status
            SectionLabel("Permissions")
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    PermissionRow(
                        title = "Display over other apps",
                        subtitle = "Required to show the mute timer popup",
                        granted = hasOverlayPermission.value,
                        onGrant = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:${context.packageName}".toUri()
                                )
                            )
                        }
                    )
                    // SCHEDULE_EXACT_ALARM is denied by default on Android 14+, so most users will
                    // see this row with a Grant button until they act on it. BootReceiver re-arms
                    // every enabled schedule as soon as the grant lands.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PermissionRow(
                            title = "Exact alarms",
                            subtitle = "Required for scheduled mutes at precise times",
                            granted = canScheduleExactAlarms.value,
                            onGrant = {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = "package:${context.packageName}".toUri()
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Privacy & Terms
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var showPrivacyPolicy by remember { mutableStateOf(false) }
                var showTermsOfUse by remember { mutableStateOf(false) }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.labelLarge.copy(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable { showPrivacyPolicy = true }
                    )
                    Text(
                        text = "Terms of Use",
                        style = MaterialTheme.typography.labelLarge.copy(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable { showTermsOfUse = true }
                    )
                }

                if (showPrivacyPolicy) {
                    AlertDialog(
                        onDismissRequest = { showPrivacyPolicy = false },
                        confirmButton = {
                            TextButton(onClick = { showPrivacyPolicy = false }) {
                                Text("Close")
                            }
                        },
                        title = { Text("Privacy Policy") },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Quiet For A Minute is committed to protecting your privacy. This application is designed to function entirely offline and does not collect, store, or transmit any personal data.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Data Collection:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "• No personal information (names, emails, addresses, etc.) is collected.\n" +
                                    "• No usage data or analytics are tracked.\n" +
                                    "• No device-specific identifiers are harvested.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Permissions:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "The app requests only the permissions necessary for its core functionality (volume control, scheduling, and overlay display). These permissions are used strictly to provide the app's features on your device and never to access your private data.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Third-Party Services:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "The app does not integrate with any third-party services, advertisers, or analytics providers.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "By using Quiet For A Minute, you agree to this simple and transparent privacy approach: your data remains your own, and stays on your device.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                }

                if (showTermsOfUse) {
                    AlertDialog(
                        onDismissRequest = { showTermsOfUse = false },
                        confirmButton = {
                            TextButton(onClick = { showTermsOfUse = false }) {
                                Text("Close")
                            }
                        },
                        title = { Text("Terms of Use") },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Acceptance of Terms",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "By downloading or using Quiet For A Minute, you agree to these terms. If you do not agree, please do not use the application.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Free Public Tool",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Quiet For A Minute is provided as a free tool for public use. It is intended for personal, non-commercial use only. The application is not used for profit in any way.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "No Warranties",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "This application is provided \"as is\" without any warranties of any kind, express or implied. While we strive for reliability, we do not guarantee that the app will be error-free or that its functions (such as volume restoration) will work perfectly on all devices or in all scenarios.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Limitation of Liability",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "In no event shall the developer be liable for any damages (including, without limitation, missed notifications, alarms, or calls) arising out of the use or inability to use this application.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "User Responsibility",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "It is your responsibility to ensure that your device's settings (such as battery optimization or notification permissions) allow the app to function as intended.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Changes to Terms",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "We may update these terms from time to time. Your continued use of the app following any changes indicates your acceptance of the new terms.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    mode: String,
    selectedMode: String,
    onClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick(mode) }
    ) {
        RadioButton(
            selected = (mode == selectedMode),
            onClick = { onClick(mode) }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.tertiary
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            if (!granted) {
                FilledTonalButton(onClick = onGrant) {
                    Text("Grant")
                }
            } else {
                Text(
                    "Granted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    )
}
