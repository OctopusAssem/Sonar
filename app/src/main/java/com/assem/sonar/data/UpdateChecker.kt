package com.assem.sonar.data

import android.content.Context
import com.assem.sonar.R
import com.assem.sonar.model.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Checks for updates against **Google Play only**.
 *
 * Google no longer exposes the app version on the public listing and its internal
 * batchexecute endpoint refuses anonymous callers, so Sonar uses the one dated fact the
 * public listing does publish: "Updated on". If Google Play lists an update date that is
 * clearly newer than the copy installed on this device, an update is reported.
 */
class UpdateChecker(private val context: Context) {

    private val ua = "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val updatedOn = Regex(
        "Updated on.{0,200}?([A-Z][a-z]{2} \\d{1,2}, 20\\d\\d)",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)

    suspend fun check(
        packageName: String,
        installedLastUpdateTime: Long,
    ): UpdateState = withContext(Dispatchers.IO) {
        if (!NetPolicy.isAllowed(context)) {
            return@withContext UpdateState.Failed(
                context.getString(R.string.update_internet_disabled),
            )
        }

        val playDate = runCatching { playUpdatedOn(packageName) }.getOrNull()
            ?: return@withContext UpdateState.Failed(
                context.getString(R.string.update_source_unreachable),
            )

        val playDay = playDate / DAY_MS
        val localDay = installedLastUpdateTime / DAY_MS

        // One day of slack absorbs the store's timezone; anything beyond that is a real update.
        if (playDay > localDay + 1) {
            UpdateState.Available(playDate, context.getString(R.string.source_play))
        } else {
            UpdateState.UpToDate
        }
    }

    /** Date of the "Updated on" field on the public Google Play listing, or null. */
    private fun playUpdatedOn(packageName: String): Long? {
        val url = "https://play.google.com/store/apps/details?id=$packageName&hl=en&gl=US"
        val body = httpGet(url) ?: return null
        if (body.contains("requested URL was not found", ignoreCase = true) ||
            body.contains("We're sorry", ignoreCase = true)
        ) {
            return null
        }
        val raw = updatedOn.find(body)?.groupValues?.get(1) ?: return null
        return runCatching { dateFmt.parse(raw)?.time }.getOrNull()
    }

    private fun httpGet(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", ua)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }
            val code = conn.responseCode
            if (code !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private companion object {
        val DAY_MS = TimeUnit.DAYS.toMillis(1)
    }
}
