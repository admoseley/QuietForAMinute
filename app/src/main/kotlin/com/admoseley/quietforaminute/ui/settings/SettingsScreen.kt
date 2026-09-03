package com.admoseley.quietforaminute.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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

    val muteChimePickerTitle = stringResource(R.string.settings_mute_chime_picker_title)
    val restoreChimePickerTitle = stringResource(R.string.settings_restore_chime_picker_title)

    fun pickRingtone(isMute: Boolean) {
        val currentUri = if (isMute) muteChimeUri else restoreChimeUri
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TITLE,
                if (isMute) muteChimePickerTitle else restoreChimePickerTitle
            )
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

    // Aggressive OEM battery managers can kill the always-on OverlayService, which is a likely
    // cause of "the popup only shows up sometimes". This exemption is declared in the manifest
    // (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) but was never surfaced in the UI until now.
    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    val ignoringBatteryOptimizations = remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
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
                ignoringBatteryOptimizations.value =
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Settings is a permanent bottom-nav tab, not a modal, so "Close" here means "get out of my
    // way" rather than "navigate back" — it backgrounds the app the same way pressing Home would.
    // The volume monitor keeps running regardless; it's a foreground service, not tied to this UI.
    val activity = LocalActivity.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = { activity?.moveTaskToBack(true) }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_close))
                    }
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
                            stringResource(R.string.settings_hero_text),
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
            SectionLabel(stringResource(R.string.settings_section_restore_volume))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (defaultVolume == 0) R.drawable.ic_volume_off
                                else if (defaultVolume < maxVolume / 2) R.drawable.ic_volume_down
                                else R.drawable.ic_volume_up
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(R.string.settings_default_volume_label),
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
                        stringResource(R.string.settings_default_volume_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Behavior settings
            SectionLabel(stringResource(R.string.settings_section_app_settings))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_app_theme)) },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                ThemeOption(stringResource(R.string.settings_theme_light), "LIGHT", themeMode) { viewModel.setThemeMode(it) }
                                ThemeOption(stringResource(R.string.settings_theme_dark), "DARK", themeMode) { viewModel.setThemeMode(it) }
                                ThemeOption(stringResource(R.string.settings_theme_system), "SYSTEM", themeMode) { viewModel.setThemeMode(it) }
                            }
                        },
                        leadingContent = {
                            Icon(painterResource(R.drawable.ic_palette), null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_show_popup_title)) },
                        supportingContent = { Text(stringResource(R.string.settings_show_popup_description)) },
                        leadingContent = {
                            Icon(painterResource(R.drawable.ic_timer), null, tint = MaterialTheme.colorScheme.primary)
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
                        headlineContent = { Text(stringResource(R.string.settings_chime_on_mute_title)) },
                        supportingContent = {
                            // Note: getRingtone()/getTitle() query a content provider on the main
                            // thread. It is a one-off per URI change so the jank is negligible,
                            // but it would belong in the ViewModel if this list grows.
                            val defaultChimeName = stringResource(R.string.settings_default_chime_name)
                            val name = remember(muteChimeUri) {
                                muteChimeUri?.let { RingtoneManager.getRingtone(context, it.toUri())?.getTitle(context) } ?: defaultChimeName
                            }
                            Text(stringResource(R.string.settings_chime_sound_format, name))
                        },
                        leadingContent = {
                            Icon(painterResource(R.drawable.ic_volume_up), null, tint = MaterialTheme.colorScheme.primary)
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
                        headlineContent = { Text(stringResource(R.string.settings_chime_on_restore_title)) },
                        supportingContent = {
                            val defaultChimeName = stringResource(R.string.settings_default_chime_name)
                            val name = remember(restoreChimeUri) {
                                restoreChimeUri?.let { RingtoneManager.getRingtone(context, it.toUri())?.getTitle(context) } ?: defaultChimeName
                            }
                            Text(stringResource(R.string.settings_chime_sound_format, name))
                        },
                        leadingContent = {
                            Icon(painterResource(R.drawable.ic_notifications_active), null, tint = MaterialTheme.colorScheme.primary)
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
            SectionLabel(stringResource(R.string.settings_section_permissions))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    PermissionRow(
                        title = stringResource(R.string.permission_overlay_title),
                        subtitle = stringResource(R.string.permission_overlay_subtitle),
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
                            title = stringResource(R.string.permission_exact_alarm_title),
                            subtitle = stringResource(R.string.permission_exact_alarm_subtitle),
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
                    PermissionRow(
                        title = stringResource(R.string.permission_battery_title),
                        subtitle = stringResource(R.string.permission_battery_subtitle),
                        granted = ignoringBatteryOptimizations.value,
                        onGrant = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            )
                        }
                    )
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
                        text = stringResource(R.string.privacy_policy_link),
                        style = MaterialTheme.typography.labelLarge.copy(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable { showPrivacyPolicy = true }
                    )
                    Text(
                        text = stringResource(R.string.terms_of_use_link),
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
                                Text(stringResource(R.string.action_close))
                            }
                        },
                        title = { Text(stringResource(R.string.privacy_policy_link)) },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stringResource(R.string.privacy_policy_intro),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.privacy_policy_data_collection_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.privacy_policy_data_collection_body),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.privacy_policy_permissions_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.privacy_policy_permissions_body),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.privacy_policy_third_party_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.privacy_policy_third_party_body),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.privacy_policy_closing),
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
                                Text(stringResource(R.string.action_close))
                            }
                        },
                        title = { Text(stringResource(R.string.terms_of_use_link)) },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stringResource(R.string.terms_acceptance_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.terms_acceptance_body),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.terms_free_tool_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.terms_free_tool_body),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.terms_no_warranties_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.terms_no_warranties_body),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.terms_liability_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.terms_liability_body),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.terms_user_responsibility_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.terms_user_responsibility_body),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.terms_changes_heading),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.terms_changes_body),
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
                painter = painterResource(if (granted) R.drawable.ic_check_circle else R.drawable.ic_warning),
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.tertiary
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = {
            if (!granted) {
                FilledTonalButton(onClick = onGrant) {
                    Text(stringResource(R.string.action_grant))
                }
            } else {
                Text(
                    stringResource(R.string.permission_granted),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    )
}
