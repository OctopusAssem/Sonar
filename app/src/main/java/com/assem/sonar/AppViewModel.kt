package com.assem.sonar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assem.sonar.data.AppRepository
import com.assem.sonar.data.NetPolicy
import com.assem.sonar.data.UpdateChecker
import com.assem.sonar.model.AppEntry
import com.assem.sonar.model.AppFilter
import com.assem.sonar.model.RootCheckResult
import com.assem.sonar.model.SortMode
import com.assem.sonar.model.UpdateState
import com.assem.sonar.root.RootChecker
import com.assem.sonar.util.LocaleHelper
import com.assem.sonar.work.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val checker = UpdateChecker(app)
    private val rootChecker = RootChecker(app)

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var usageAccess by mutableStateOf(false)
        private set
    var usageDays by mutableStateOf(30)
        private set

    var query by mutableStateOf("")
    var filter by mutableStateOf(AppFilter.ALL)
    var sort by mutableStateOf(SortMode.NAME)

    var rootResults by mutableStateOf<List<RootCheckResult>>(emptyList())
        private set
    var rootRunning by mutableStateOf(false)
        private set

    var checkingAll by mutableStateOf(false)
        private set
    var checkProgress by mutableStateOf("")
        private set

    var internetAllowed by mutableStateOf(NetPolicy.isAllowed(app))
        private set
    var backgroundCheck by mutableStateOf(WorkScheduler.isEnabled(app))
        private set
    var language by mutableStateOf(LocaleHelper.get(app))
        private set

    fun load() {
        viewModelScope.launch {
            loading = true
            usageAccess = repo.hasUsageAccess()
            val list = withContext(Dispatchers.IO) { repo.loadInstalledApps() }
            val usage = if (usageAccess) {
                withContext(Dispatchers.IO) { repo.loadUsage(usageDays) }
            } else {
                emptyMap()
            }
            apps = list.map { AppEntry(it, usage[it.packageName]) }
            loading = false
        }
    }

    fun refreshUsage(days: Int = usageDays) {
        viewModelScope.launch {
            usageDays = days
            usageAccess = repo.hasUsageAccess()
            if (!usageAccess) return@launch
            val usage = withContext(Dispatchers.IO) { repo.loadUsage(days) }
            apps = apps.map { it.copy(usage = usage[it.info.packageName]) }
        }
    }

    fun visibleApps(): List<AppEntry> {
        val q = query.trim().lowercase()
        val list = apps.filter { entry ->
            val okFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !entry.info.isSystem
                AppFilter.SYSTEM -> entry.info.isSystem
            }
            val okQuery = q.isEmpty() ||
                entry.info.label.lowercase().contains(q) ||
                entry.info.packageName.lowercase().contains(q)
            okFilter && okQuery
        }
        return when (sort) {
            SortMode.NAME -> list.sortedBy { it.info.label.lowercase() }
            SortMode.INSTALL_DATE -> list.sortedByDescending { it.info.firstInstallTime }
            SortMode.UPDATE_DATE -> list.sortedByDescending { it.info.lastUpdateTime }
            SortMode.USAGE -> list.sortedByDescending { it.usage?.totalTimeInForegroundMs ?: 0L }
            SortMode.SIZE -> list.sortedByDescending { it.info.apkSizeBytes }
        }
    }

    fun updatesAvailable(): List<AppEntry> =
        apps.filter { it.update is UpdateState.Available }

    fun checkOne(packageName: String) {
        val entry = apps.firstOrNull { it.info.packageName == packageName } ?: return
        setUpdate(packageName, UpdateState.Checking)
        viewModelScope.launch {
            val state = checker.check(
                entry.info.packageName, entry.info.versionName, entry.info.versionCode,
            )
            setUpdate(packageName, state)
        }
    }

    fun checkAll() {
        if (checkingAll || !internetAllowed) return
        viewModelScope.launch {
            checkingAll = true
            val targets = apps.filter { !it.info.isSystem }.take(60)
            var done = 0
            for (entry in targets) {
                setUpdate(entry.info.packageName, UpdateState.Checking)
                val state = checker.check(
                    entry.info.packageName, entry.info.versionName, entry.info.versionCode,
                )
                setUpdate(entry.info.packageName, state)
                done++
                checkProgress = "$done / ${targets.size}"
            }
            checkProgress = ""
            checkingAll = false
        }
    }

    fun runRootChecks() {
        if (rootRunning) return
        viewModelScope.launch {
            rootRunning = true
            rootResults = withContext(Dispatchers.IO) { rootChecker.runAll() }
            rootRunning = false
        }
    }

    fun updateInternetAllowed(allowed: Boolean) {
        NetPolicy.setAllowed(getApplication(), allowed)
        internetAllowed = allowed
        if (!allowed) {
            WorkScheduler.setEnabled(getApplication(), false)
            backgroundCheck = false
        }
    }

    fun updateBackgroundCheck(enabled: Boolean) {
        WorkScheduler.setEnabled(getApplication(), enabled)
        backgroundCheck = WorkScheduler.isEnabled(getApplication())
    }

    fun changeLanguage(code: String) {
        LocaleHelper.set(getApplication(), code)
        language = code
    }

    private fun setUpdate(packageName: String, state: UpdateState) {
        apps = apps.map {
            if (it.info.packageName == packageName) it.copy(update = state) else it
        }
    }
}
