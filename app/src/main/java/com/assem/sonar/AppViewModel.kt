package com.assem.sonar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assem.sonar.data.AppRepository
import com.assem.sonar.data.FreezeManager
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

/** State of an explicit "request root permission" action. */
enum class RootRequestState { IDLE, REQUESTING, GRANTED, DENIED }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val checker = UpdateChecker(app)
    private val rootChecker = RootChecker(app)

    /** Localized context that follows the in-app language choice. */
    private val localized get() = LocaleHelper.wrap(getApplication())

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

    /** null = not checked yet. */
    var rootAvailable by mutableStateOf<Boolean?>(null)
        private set
    var rootChecking by mutableStateOf(false)
        private set

    /** Result of the explicit "request root permission" action. */
    var rootRequestState by mutableStateOf(RootRequestState.IDLE)
        private set

    /** App whose freeze is waiting for the user to grant root; drives the "ask for root" dialog. */
    var rootPromptEntry by mutableStateOf<AppEntry?>(null)
        private set

    /** Package currently being frozen or unfrozen. */
    var freezeBusy by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
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
            val state = checker.check(entry.info.packageName, entry.info.lastUpdateTime)
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
                val state = checker.check(entry.info.packageName, entry.info.lastUpdateTime)
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

    fun checkRootAccess() {
        if (rootChecking) return
        viewModelScope.launch {
            rootChecking = true
            rootAvailable = withContext(Dispatchers.IO) { FreezeManager.hasRoot() }
            rootChecking = false
        }
    }

    fun dismissRootPrompt() {
        rootPromptEntry = null
    }

    /**
     * Asks the root manager for access on the user's request.
     *
     * [retry] is the app whose freeze triggered the request; when root is granted the freeze is
     * retried automatically so the user does not have to tap again.
     */
    fun requestRootPermission(retry: AppEntry? = null) {
        if (rootRequestState == RootRequestState.REQUESTING) return
        rootPromptEntry = null
        viewModelScope.launch {
            rootRequestState = RootRequestState.REQUESTING
            val granted = withContext(Dispatchers.IO) { FreezeManager.requestRoot() }
            rootRequestState = if (granted) RootRequestState.GRANTED else RootRequestState.DENIED
            rootAvailable = granted
            if (granted) {
                message = localized.getString(R.string.root_request_granted)
                if (retry != null) changeFrozenState(retry, freeze = true)
            } else {
                message = localized.getString(R.string.root_request_denied)
            }
        }
    }

    fun freeze(entry: AppEntry) = changeFrozenState(entry, freeze = true)

    fun unfreeze(entry: AppEntry) = changeFrozenState(entry, freeze = false)

    private fun changeFrozenState(entry: AppEntry, freeze: Boolean) {
        val pkg = entry.info.packageName
        if (pkg == getApplication<Application>().packageName) {
            message = localized.getString(R.string.freeze_cannot_self)
            return
        }
        if (freezeBusy != null) return
        viewModelScope.launch {
            freezeBusy = pkg
            val result = withContext(Dispatchers.IO) {
                if (freeze) FreezeManager.freeze(pkg) else FreezeManager.unfreeze(pkg)
            }
            freezeBusy = null
            if (result is FreezeManager.Result.NoRoot) rootPromptEntry = entry
            message = when (result) {
                is FreezeManager.Result.Success -> localized.getString(
                    if (freeze) R.string.freeze_success else R.string.unfreeze_success,
                    entry.info.label,
                )
                is FreezeManager.Result.NoRoot -> localized.getString(R.string.freeze_needs_root)
                is FreezeManager.Result.Failure -> localized.getString(
                    if (freeze) R.string.freeze_failed else R.string.unfreeze_failed,
                    entry.info.label,
                    result.detail,
                )
            }
            if (result is FreezeManager.Result.Success) {
                rootAvailable = true
                reload()
            }
        }
    }

    fun clearMessage() {
        message = null
    }

    private suspend fun reload() {
        val list = withContext(Dispatchers.IO) { repo.loadInstalledApps() }
        val usage = if (usageAccess) {
            withContext(Dispatchers.IO) { repo.loadUsage(usageDays) }
        } else {
            emptyMap()
        }
        apps = list.map { AppEntry(it, usage[it.packageName]) }
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
