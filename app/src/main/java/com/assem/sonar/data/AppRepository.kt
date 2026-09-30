package com.assem.sonar.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.assem.sonar.model.AppInfo
import com.assem.sonar.model.UsageInfo

class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun loadInstalledApps(): List<AppInfo> {
        val flags = PackageManager.GET_META_DATA
        val packages = pm.getInstalledPackages(flags)
        val result = ArrayList<AppInfo>(packages.size)
        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val label = runCatching { pm.getApplicationLabel(appInfo).toString() }
                .getOrDefault(pkg.packageName)
            val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
            val size = runCatching { java.io.File(appInfo.sourceDir ?: "").length() }.getOrDefault(0L)
            result += AppInfo(
                packageName = pkg.packageName,
                label = label,
                icon = icon,
                isSystem = isSystem,
                isEnabled = appInfo.enabled,
                versionName = pkg.versionName ?: "-",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION") pkg.versionCode.toLong()
                },
                firstInstallTime = pkg.firstInstallTime,
                lastUpdateTime = pkg.lastUpdateTime,
                apkSizeBytes = size,
                uid = appInfo.uid,
            )
        }
        return result.sortedBy { it.label.lowercase() }
    }

    fun hasUsageAccess(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Throwable) {
            false
        }
    }

    /** Aggregated foreground time over the last [days] days, keyed by package name. */
    fun loadUsage(days: Int = 30): Map<String, UsageInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val begin = end - days * 24L * 60L * 60L * 1000L
        val stats = runCatching {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
        }.getOrNull() ?: return emptyMap()

        val time = HashMap<String, Long>()
        val last = HashMap<String, Long>()
        for (s in stats) {
            if (s.totalTimeInForeground <= 0L) continue
            time[s.packageName] = (time[s.packageName] ?: 0L) + s.totalTimeInForeground
            val prev = last[s.packageName] ?: 0L
            if (s.lastTimeUsed > prev) last[s.packageName] = s.lastTimeUsed
        }
        return time.mapValues { (pkg, total) ->
            UsageInfo(totalTimeInForegroundMs = total, lastTimeUsed = last[pkg] ?: 0L)
        }
    }

    fun isLaunchable(packageName: String): Boolean {
        return pm.getLaunchIntentForPackage(packageName) != null
    }
}
