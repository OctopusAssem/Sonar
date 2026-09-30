package com.assem.sonar.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val isEnabled: Boolean,
    val versionName: String,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val apkSizeBytes: Long,
    val uid: Int,
)

data class UsageInfo(
    val totalTimeInForegroundMs: Long,
    val lastTimeUsed: Long,
)

sealed interface UpdateState {
    data object Unknown : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState

    /** Google Play lists a newer update date than the copy installed on this device. */
    data class Available(
        val playUpdatedOn: Long,
        val source: String,
    ) : UpdateState

    data class Failed(val reason: String) : UpdateState
}

data class AppEntry(
    val info: AppInfo,
    val usage: UsageInfo?,
    val update: UpdateState = UpdateState.Unknown,
)

data class RootCheckResult(
    val category: String,
    val name: String,
    val detected: Boolean,
    val detail: String,
)

enum class AppFilter { ALL, USER, SYSTEM }

enum class SortMode { NAME, INSTALL_DATE, UPDATE_DATE, USAGE, SIZE }

enum class Tab { APPS, UPDATES, ROOT, SETTINGS }
