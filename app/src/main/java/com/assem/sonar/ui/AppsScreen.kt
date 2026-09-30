package com.assem.sonar.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.assem.sonar.AppViewModel
import com.assem.sonar.R
import com.assem.sonar.model.AppEntry
import com.assem.sonar.model.AppFilter
import com.assem.sonar.model.SortMode
import com.assem.sonar.model.UpdateState
import com.assem.sonar.util.formatDateTime
import com.assem.sonar.util.formatDuration
import com.assem.sonar.util.formatSize
import com.assem.sonar.util.relativeTime
import com.assem.sonar.util.toImageBitmap

@Composable
private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.NAME -> stringResource(R.string.sort_name)
    SortMode.INSTALL_DATE -> stringResource(R.string.sort_install)
    SortMode.UPDATE_DATE -> stringResource(R.string.sort_update)
    SortMode.USAGE -> stringResource(R.string.sort_usage)
    SortMode.SIZE -> stringResource(R.string.sort_size)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<AppEntry?>(null) }
    var showSort by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf<AppEntry?>(null) }

    Column(Modifier.fillMaxSize()) {
        if (!vm.usageAccess) {
            UsageAccessBanner(context)
        }
        OutlinedTextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppFilter.entries.forEach { f ->
                val label = when (f) {
                    AppFilter.ALL -> stringResource(R.string.filter_all)
                    AppFilter.USER -> stringResource(R.string.filter_user)
                    AppFilter.SYSTEM -> stringResource(R.string.filter_system)
                }
                FilterChip(
                    selected = vm.filter == f,
                    onClick = { vm.filter = f },
                    label = { Text(label) },
                )
            }
            Box(Modifier.weight(1f))
            Box {
                AssistChip(
                    onClick = { showSort = true },
                    label = { Text(sortLabel(vm.sort)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                )
                DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(mode)) },
                            onClick = { vm.sort = mode; showSort = false },
                        )
                    }
                }
            }
        }
        val visible = vm.visibleApps()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                pluralStringResource(R.plurals.app_count, visible.size, visible.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.tap_app_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (vm.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.loading_apps))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(visible, key = { it.info.packageName }) { entry ->
                    AppRow(entry, Icons.Default.Android) { selected = entry }
                }
            }
        }
    }

    selected?.let { entry ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = sheetState) {
            DetailContent(
                entry = entry,
                vm = vm,
                onUninstall = { confirmUninstall = entry; selected = null },
            )
        }
    }

    confirmUninstall?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmUninstall = null },
            title = { Text(stringResource(R.string.uninstall_dialog_title)) },
            text = {
                Text(stringResource(R.string.uninstall_dialog_body, entry.info.label))
            },
            confirmButton = {
                TextButton(onClick = {
                    uninstall(context, entry.info.packageName)
                    confirmUninstall = null
                }) { Text(stringResource(R.string.dialog_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUninstall = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun UsageAccessBanner(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.usage_access_missing_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.usage_access_missing_body),
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }) { Text(stringResource(R.string.open_settings)) }
        }
    }
}

@Composable
private fun DetailContent(
    entry: AppEntry,
    vm: AppViewModel,
    onUninstall: () -> Unit,
) {
    val context = LocalContext.current
    val info = entry.info
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(info.icon.toImageBitmap(), Icons.Default.Android)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    info.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    info.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                KeyValue(stringResource(R.string.label_version), "${info.versionName}  (${info.versionCode})")
                KeyValue(stringResource(R.string.label_install_date), formatDateTime(info.firstInstallTime))
                KeyValue(
                    stringResource(R.string.label_last_update),
                    "${formatDateTime(info.lastUpdateTime)}  ·  ${relativeTime(context, info.lastUpdateTime)}",
                )
                KeyValue(stringResource(R.string.label_apk_size), formatSize(info.apkSizeBytes))
                KeyValue(
                    stringResource(R.string.label_type),
                    stringResource(if (info.isSystem) R.string.type_system else R.string.type_user),
                )
                KeyValue(stringResource(R.string.label_uid), info.uid.toString())
                entry.usage?.let {
                    KeyValue(
                        stringResource(R.string.label_usage_time, vm.usageDays),
                        formatDuration(context, it.totalTimeInForegroundMs),
                    )
                    KeyValue(stringResource(R.string.label_last_used), formatDateTime(it.lastTimeUsed))
                }
            }
        }

        when (val u = entry.update) {
            is UpdateState.Available -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.update_available, u.latestVersionName),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.source_label, u.source),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            UpdateState.UpToDate -> Text(
                stringResource(R.string.up_to_date_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            is UpdateState.Failed -> Text(
                u.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            else -> Unit
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.checkOne(info.packageName) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("  " + stringResource(R.string.action_check_update))
            }
            OutlinedButton(
                onClick = { openApp(context, info.packageName) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Text("  " + stringResource(R.string.action_open))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { openAppInfo(context, info.packageName) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Text("  " + stringResource(R.string.action_system_info))
            }
            OutlinedButton(
                onClick = { openPlayStore(context, info.packageName) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Shop, contentDescription = null)
                Text("  " + stringResource(R.string.action_google_play))
            }
        }
        Button(
            onClick = onUninstall,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Text("  " + stringResource(R.string.action_uninstall))
        }
    }
}

fun openApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent == null) {
        openAppInfo(context, packageName)
        return
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun openAppInfo(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

fun openPlayStore(context: Context, packageName: String) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

fun uninstall(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
