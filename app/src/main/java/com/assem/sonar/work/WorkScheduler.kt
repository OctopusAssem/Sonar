package com.assem.sonar.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.assem.sonar.data.NetPolicy
import com.assem.sonar.util.Prefs
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val UNIQUE_NAME = "sonar_update_check"
    private const val KEY_ENABLED = "background_check_enabled"

    fun isEnabled(context: Context): Boolean =
        Prefs.of(context).getBoolean(KEY_ENABLED, false) && NetPolicy.isAllowed(context)

    fun setEnabled(context: Context, enabled: Boolean) {
        Prefs.of(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled && NetPolicy.isAllowed(context)) enable(context) else disable(context)
    }

    private fun enable(context: Context) {
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}
