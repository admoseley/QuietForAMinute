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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val muteChimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.setMuteChimeUri(uri?.toString())
        }
    }

    val restoreChimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.setRestoreChimeUri(uri?.toString())
        }
    }

    fun pickRingtone(isMute: Boolean) {
        val currentUri = if (isMute) muteChimeUri else restoreChimeUri
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, if (isMute) "Mute Chime" else "Restore Chime")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri?.let { Uri.parse(it) })
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
                        modifier = Modifier.size(64.dp)
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
                            val name = remember(muteChimeUri) {
                                muteChimeUri?.let { RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) } ?: "Default chime"
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
                                restoreChimeUri?.let { RingtoneManager.getRingtone(context, Uri.parse(it))?.getTitle(context) } ?: "Default chime"
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
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PermissionRow(
                            title = "Exact alarms",
                            subtitle = "Required for scheduled mutes at precise times",
                            granted = canScheduleExactAlarms.value,
                            onGrant = {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        )
                    }
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
