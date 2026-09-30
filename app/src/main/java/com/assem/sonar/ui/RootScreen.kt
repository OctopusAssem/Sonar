package com.assem.sonar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.assem.sonar.AppViewModel
import com.assem.sonar.R
import com.assem.sonar.model.RootCheckResult

@Composable
fun RootScreen(vm: AppViewModel) {
    val detected = vm.rootResults.count { it.detected }
    Column(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    vm.rootResults.isEmpty() -> MaterialTheme.colorScheme.surfaceVariant
                    detected > 0 -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
            ),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (detected > 0) Icons.Default.GppMaybe else Icons.Default.GppGood,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        when {
                            vm.rootResults.isEmpty() -> stringResource(R.string.root_header_idle)
                            detected > 0 -> stringResource(R.string.root_header_found, detected)
                            else -> stringResource(R.string.root_header_clean)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    stringResource(R.string.root_body),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (vm.rootRunning) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button(onClick = { vm.runRootChecks() }, enabled = !vm.rootRunning) {
                    Icon(Icons.Default.Shield, contentDescription = null)
                    Text("  " + stringResource(R.string.root_start_scan))
                }
            }
        }

        if (vm.rootResults.isEmpty() && !vm.rootRunning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.root_tap_to_start),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(vm.rootResults) { result -> RootResultRow(result) }
            }
        }
    }
}

@Composable
private fun RootResultRow(result: RootCheckResult) {
    val color = if (result.detected) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (result.detected) Icons.Default.GppMaybe else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp),
            )
            Column(
                Modifier.weight(1f).padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    result.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    result.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(
                stringResource(
                    if (result.detected) R.string.root_status_found else R.string.root_status_clean,
                ),
                color,
            )
        }
    }
}
