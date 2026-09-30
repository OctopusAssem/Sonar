package com.assem.sonar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.assem.sonar.data.AppRepository
import com.assem.sonar.data.NetPolicy
import com.assem.sonar.data.UpdateChecker
import com.assem.sonar.model.UpdateState
import com.assem.sonar.notify.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!NetPolicy.isAllowed(applicationContext)) return@withContext Result.success()

        val repo = AppRepository(applicationContext)
        val checker = UpdateChecker(applicationContext)
        val apps = repo.loadInstalledApps()
            .filter { !it.isSystem }
            .take(25)

        val available = ArrayList<String>()
        for (app in apps) {
            val state = runCatching {
                checker.check(app.packageName, app.versionName, app.versionCode)
            }.getOrNull()
            if (state is UpdateState.Available) {
                available += "${app.label} → ${state.latestVersionName}"
            }
            delay(400)
        }

        if (available.isNotEmpty()) {
            Notifications.showUpdates(
                applicationContext,
                available.size,
                available.take(5).joinToString("\n"),
            )
        }
        Result.success()
    }
}
