package com.assem.sonar

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assem.sonar.model.Tab
import com.assem.sonar.notify.Notifications
import com.assem.sonar.ui.AppsScreen
import com.assem.sonar.ui.RootScreen
import com.assem.sonar.ui.SettingsScreen
import com.assem.sonar.ui.SonarTheme
import com.assem.sonar.ui.UpdatesScreen
import com.assem.sonar.util.LocaleHelper

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.ensureChannel(this)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { }

        setContent {
            SonarTheme {
                val vm: AppViewModel = viewModel()
                LifecycleEventEffect(Lifecycle.Event.ON_START) {
                    vm.load()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !Notifications.canNotify(this@MainActivity)
                    ) {
                        runCatching {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
                RootScaffold(vm)
            }
        }
    }
}

private data class TabItem(val tab: Tab, val labelRes: Int, val icon: ImageVector)

@Composable
private fun RootScaffold(vm: AppViewModel) {
    var current by rememberSaveable { mutableStateOf(Tab.APPS) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        val msg = vm.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.clearMessage()
    }
    val tabs = remember {
        listOf(
            TabItem(Tab.APPS, R.string.tab_apps, Icons.Default.Apps),
            TabItem(Tab.UPDATES, R.string.tab_updates, Icons.Default.SystemUpdate),
            TabItem(Tab.ROOT, R.string.tab_root, Icons.Default.Shield),
            TabItem(Tab.SETTINGS, R.string.tab_settings, Icons.Default.Settings),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { item ->
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = current == item.tab,
                        onClick = { current = item.tab },
                        icon = { Icon(item.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (current) {
                Tab.APPS -> AppsScreen(vm)
                Tab.UPDATES -> UpdatesScreen(vm)
                Tab.ROOT -> RootScreen(vm)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}
