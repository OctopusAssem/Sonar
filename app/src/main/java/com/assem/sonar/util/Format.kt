package com.assem.sonar.util

import android.content.Context
import com.assem.sonar.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
private val dateOnlyFmt = SimpleDateFormat("yyyy/MM/dd", Locale.US)

fun formatDateTime(millis: Long): String =
    if (millis <= 0L) "-" else dateFmt.format(Date(millis))

fun formatDate(millis: Long): String =
    if (millis <= 0L) "-" else dateOnlyFmt.format(Date(millis))

fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val units = arrayOf("B", "KB", "MB", "GB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024.0
        i++
    }
    return if (i == 0) "$bytes ${units[0]}"
    else String.format(Locale.US, "%.1f %s", v, units[i])
}

fun formatDuration(context: Context, ms: Long): String {
    if (ms <= 0L) return "-"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return when {
        h > 0 -> context.getString(R.string.duration_hours_minutes, h, m)
        m > 0 -> context.getString(R.string.duration_minutes, m)
        else -> context.getString(R.string.duration_seconds, totalSec)
    }
}

fun relativeTime(context: Context, millis: Long): String {
    if (millis <= 0L) return "-"
    val diff = System.currentTimeMillis() - millis
    val day = 24L * 60L * 60L * 1000L
    return when {
        diff < 60_000 -> context.getString(R.string.time_now)
        diff < 3_600_000 -> context.getString(R.string.time_minutes_ago, diff / 60_000)
        diff < day -> context.getString(R.string.time_hours_ago, diff / 3_600_000)
        diff < 30 * day -> context.getString(R.string.time_days_ago, diff / day)
        else -> formatDate(millis)
    }
}
