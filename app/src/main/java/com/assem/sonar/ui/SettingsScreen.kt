package com.assem.sonar.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.assem.sonar.AppViewModel
import com.assem.sonar.R
import com.assem.sonar.notify.Notifications
import com.assem.sonar.util.LocaleHelper

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var notifyGranted by remember { mutableStateOf(Notifications.canNotify(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notifyGranted = granted }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsCard(stringResource(R.string.settings_language_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    LocaleHelper.SYSTEM to R.string.language_system,
                    LocaleHelper.ARABIC to R.string.language_arabic,
                    LocaleHelper.ENGLISH to R.string.language_english,
                ).forEach { (code, labelRes) ->
                    FilterChip(
                        selected = vm.language == code,
                        onClick = {
                            if (vm.language != code) {
                                vm.changeLanguage(code)
                                (context as? Activity)?.recreate()
                            }
                        },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }
        }

        SettingsCard(stringResource(R.string.settings_internet_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_internet_allow),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(
                            if (vm.internetAllowed) R.string.settings_internet_body_on
                            else R.string.settings_internet_body_off,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = vm.internetAllowed,
                    onCheckedChange = { vm.updateInternetAllowed(it) },
                )
            }
        }

        SettingsCard(stringResource(R.string.settings_root_title)) {
            Text(
                stringResource(
                    when (vm.rootAvailable) {
                        true -> R.string.settings_root_granted
                        false -> R.string.settings_root_denied
                        null -> R.string.settings_root_unknown
                    },
                ),
                color = when (vm.rootAvailable) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.settings_root_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (vm.rootChecking) {
                Text(
                    stringResource(R.string.freeze_checking_root),
                    style = MaterialTheme.typography.labelMedium,
                )
            } else {
                OutlinedButton(onClick = { vm.checkRootAccess() }) {
                    Text(stringResource(R.string.settings_root_request))
                }
            }
        }

        SettingsCard(stringResource(R.string.settings_usage_title)) {
            Text(
                stringResource(
                    if (vm.usageAccess) R.string.settings_granted else R.string.settings_not_granted,
                ),
                color = if (vm.usageAccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.settings_usage_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            }) { Text(stringResource(R.string.settings_open_usage)) }
        }

        SettingsCard(stringResource(R.string.settings_period_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { days ->
                    FilterChip(
                        selected = vm.usageDays == days,
                        onClick = { vm.refreshUsage(days) },
                        label = { Text(stringResource(R.string.settings_days, days)) },
                    )
                }
            }
        }

        SettingsCard(stringResource(R.string.settings_notifications_title)) {
            Text(
                stringResource(
                    when {
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
                            R.string.settings_notif_legacy
                        notifyGranted -> R.string.settings_notif_granted
                        else -> R.string.settings_notif_denied
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifyGranted) {
                OutlinedButton(onClick = {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.settings_allow_notifications)) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_background_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.settings_background_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = vm.backgroundCheck,
                    enabled = vm.internetAllowed,
                    onCheckedChange = { vm.updateBackgroundCheck(it) },
                )
            }
        }

        SettingsCard(stringResource(R.string.settings_about_title)) {
            KeyValue(stringResource(R.string.about_name), stringResource(R.string.app_name))
            KeyValue(stringResource(R.string.about_package), context.packageName)
            KeyValue(
                stringResource(R.string.about_version),
                runCatching {
                    val p = context.packageManager.getPackageInfo(context.packageName, 0)
                    "${p.versionName} (${p.longVersionCode})"
                }.getOrDefault("-"),
            )
            KeyValue(
                stringResource(R.string.about_developer),
                stringResource(R.string.about_developer_value),
            )
            Text(
                stringResource(R.string.about_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
